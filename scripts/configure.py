# This script generates and updates project configuration files.

# Run this script with rvscaffold in PYTHONPATH
import rvscaffold as scaffold

class Project(scaffold.Java):
    def script_path_text(self): return __file__
    def repository_name(self): return 'sourceafis-java'
    def pretty_name(self): return 'SourceAFIS for Java'
    def pom_name(self): return 'SourceAFIS'
    def pom_description(self): return 'Fingerprint recognition engine that takes a pair of human fingerprint images and returns their similarity score. Supports efficient 1:N search.'
    def inception_year(self): return 2009
    def homepage(self): return self.website() + 'java'
    def stagean_annotations(self): return True
    def project_status(self): return self.stable_status()
    
    def dependencies(self):
        yield from super().dependencies()
        yield self.use_closeablescope()
        yield self.use_noexception()
        yield self.use('com.machinezoo.fingerprintio:fingerprintio:1.3.0')
        yield self.use_fastutil()
        yield self.use_commons_io()
        yield self.use_gson()
        yield from self.use_jackson_cbor()
        yield self.use('com.github.mhshams:jnbis:2.1.1')
        yield self.use_junit()
        yield self.use_hamcrest()
    
    def javadoc_links(self):
        yield from super().javadoc_links()
        yield 'https://closeablescope.machinezoo.com/javadoc/'
        yield 'https://noexception.machinezoo.com/javadocs/core/'
        yield 'https://fingerprintio.machinezoo.com/javadoc/'
    
    def documentation_links(self):
        yield 'SourceAFIS for Java', self.homepage()
        yield 'Javadoc', self.javadoc_home()
        yield 'SourceAFIS overview', 'https://sourceafis.machinezoo.com/'
        yield 'Algorithm', 'https://sourceafis.machinezoo.com/algorithm'

Project().generate()
